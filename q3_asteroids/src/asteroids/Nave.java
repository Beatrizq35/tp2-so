package asteroids;

public class Nave {
    public double x, y, vx, vy, ang = -Math.PI / 2;
    public double cooldown, invulneravel;
    public final double raio = 10;
    // escritas pela thread de interface (teclado), lidas pela thread de lógica
    public volatile boolean esq, dir, acel, atirando;
    private final double ix, iy;

    public Nave(double x, double y) { this.x = ix = x; this.y = iy = y; }

    public void reiniciar() { x = ix; y = iy; vx = vy = 0; ang = -Math.PI / 2; }

    public void atualizar(double dt, int w, int h) {
        if (esq) ang -= 3.5 * dt;
        if (dir) ang += 3.5 * dt;
        if (acel) { vx += Math.cos(ang) * 250 * dt; vy += Math.sin(ang) * 250 * dt; }
        double atrito = Math.pow(0.5, dt);
        vx *= atrito; vy *= atrito;
        x += vx * dt; y += vy * dt;
        if (x < 0) x += w; else if (x > w) x -= w;
        if (y < 0) y += h; else if (y > h) y -= h;
        if (cooldown > 0) cooldown -= dt;
        if (invulneravel > 0) invulneravel -= dt;
    }
}