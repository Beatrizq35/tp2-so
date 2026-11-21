package asteroids;

import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Estado e regras do jogo. As DUAS versões (sequencial e paralela) usam esta mesma classe,
 * então fazem exatamente o mesmo trabalho: só muda QUEM executa cada parte.
 */
public class Mundo {
    public final int largura, altura, inicial, maximo;
    public final List<Asteroide> asteroides = new CopyOnWriteArrayList<>();
    public final List<Tiro> tiros = new CopyOnWriteArrayList<>();
    public final Queue<Asteroide> pendentes = new ConcurrentLinkedQueue<>(); // fila produtor (Spawner) -> consumidor (Lógica)
    public final Nave nave;
    public volatile int pontos = 0, vidas = 3;
    public volatile double msLogica = 0;
    public Consumer<String> log = s -> { };
    private final Random rnd;
    private final AtomicInteger delta = new AtomicInteger(); // +/- asteroides pedidos pelo teclado

    public Mundo(int largura, int altura, int inicial, long seed) {
        this.largura = largura; this.altura = altura;
        this.inicial = inicial; this.maximo = inicial * 3;
        this.rnd = new Random(seed);
        this.nave = new Nave(largura / 2.0, altura / 2.0);
        criarOnda();
    }

    public void criarOnda() {
        asteroides.clear();
        for (int i = 0; i < inicial; i++) asteroides.add(novoAsteroide(true));
    }

    public Asteroide novoAsteroide(boolean qualquerLugar) {
        double raio = 8 + rnd.nextDouble() * 18;
        double vel = 20 + rnd.nextDouble() * 70;
        double dir = rnd.nextDouble() * Math.PI * 2;
        double x, y;
        if (qualquerLugar) {
            do { x = rnd.nextDouble() * largura; y = rnd.nextDouble() * altura; }
            while (Math.hypot(x - nave.x, y - nave.y) < 150);
        } else { x = 0; y = rnd.nextDouble() * altura; }
        return new Asteroide(x, y, Math.cos(dir) * vel, Math.sin(dir) * vel, raio, (rnd.nextDouble() - 0.5) * 3);
    }

    public void adicionarAsteroides(int k) { delta.addAndGet(k); }
    public void removerAsteroides(int k) { delta.addAndGet(-k); }

    // ---------- Parte PARALELIZÁVEL: cada "parte" (thread) cuida de uma fatia da lista ----------

    /** Fase 1: move a fatia 'parte' de 'partes'. Retorna quantos asteroides processou. */
    public int moverAsteroides(int parte, int partes, double dt) {
        Asteroide[] a = asteroides.toArray(new Asteroide[0]);
        int de = (int) ((long) a.length * parte / partes);
        int ate = (int) ((long) a.length * (parte + 1) / partes);
        for (int i = de; i < ate; i++) { a[i].mover(dt, largura, altura); a[i].dono = parte; }
        return ate - de;
    }

    /** Fase 2 (custo O(n²)): conta sobreposições de cada asteroide da fatia com todos os outros. */
    public void interagirAsteroides(int parte, int partes) {
        Asteroide[] a = asteroides.toArray(new Asteroide[0]);
        int de = (int) ((long) a.length * parte / partes);
        int ate = (int) ((long) a.length * (parte + 1) / partes);
        for (int i = de; i < ate; i++) {
            Asteroide ai = a[i];
            int c = 0;
            for (int j = 0; j < a.length; j++) {
                if (j == i) continue;
                double dx = ai.x - a[j].x, dy = ai.y - a[j].y, r = ai.raio + a[j].raio;
                if (dx * dx + dy * dy < r * r) c++;
            }
            ai.contatos = c;
        }
    }

    // ---------- Parte SERIAL (uma única thread): nave, tiros, colisões, pontuação ----------

    public void passoJogo(double dt) {
        Asteroide p;
        while ((p = pendentes.poll()) != null) asteroides.add(p);
        int d = delta.getAndSet(0);
        for (int i = 0; i < d; i++) asteroides.add(novoAsteroide(false));
        for (int i = 0; i < -d && !asteroides.isEmpty(); i++) asteroides.remove(asteroides.size() - 1);

        nave.atualizar(dt, largura, altura);
        if (nave.atirando && nave.cooldown <= 0) {
            double c = Math.cos(nave.ang), s = Math.sin(nave.ang);
            tiros.add(new Tiro(nave.x + c * 12, nave.y + s * 12, nave.vx + c * 450, nave.vy + s * 450));
            nave.cooldown = 0.2;
            log.accept("Tiro disparado (tiros ativos: " + tiros.size() + ")");
        }
        for (Tiro t : tiros) { t.mover(dt, largura, altura); if (t.vida <= 0) tiros.remove(t); }

        // tiro x asteroide
        for (Tiro t : tiros) {
            for (Asteroide a : asteroides) {
                double dx = t.x - a.x, dy = t.y - a.y;
                if (dx * dx + dy * dy < a.raio * a.raio) {
                    tiros.remove(t); asteroides.remove(a); pontos += 10;
                    if (a.raio > 14) { asteroides.add(filho(a)); asteroides.add(filho(a)); }
                    log.accept("Asteroide destruído (raio " + (int) a.raio + "). Restam " + asteroides.size());
                    break;
                }
            }
        }

        // nave x asteroide
        if (nave.invulneravel <= 0) {
            for (Asteroide a : asteroides) {
                if (Math.hypot(nave.x - a.x, nave.y - a.y) < a.raio + nave.raio) {
                    vidas--; nave.reiniciar(); nave.invulneravel = 2.0;
                    log.accept("Nave atingida! Vidas: " + vidas);
                    if (vidas <= 0) { log.accept("GAME OVER - reiniciando"); vidas = 3; pontos = 0; }
                    break;
                }
            }
        }
        if (asteroides.isEmpty()) { log.accept("Onda limpa! Nova onda."); criarOnda(); }
    }

    private Asteroide filho(Asteroide a) {
        double vel = 40 + rnd.nextDouble() * 70, dir = rnd.nextDouble() * Math.PI * 2;
        return new Asteroide(a.x, a.y, Math.cos(dir) * vel, Math.sin(dir) * vel, a.raio * 0.6, (rnd.nextDouble() - 0.5) * 4);
    }
}