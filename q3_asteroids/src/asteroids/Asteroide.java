package asteroids;

/** Um asteroide. Cada thread de atualização escreve apenas nos seus próprios asteroides. */
public class Asteroide {
    public double x, y, vx, vy, ang, vang;
    public final double raio;
    public volatile int dono = -1;   // índice da thread que moveu o asteroide (usado para colorir)
    public volatile int contatos;    // quantos asteroides estão sobrepostos a este

    public Asteroide(double x, double y, double vx, double vy, double raio, double vang) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.raio = raio; this.vang = vang;
    }

    public void mover(double dt, int w, int h) {
        x += vx * dt; y += vy * dt; ang += vang * dt;
        if (x < 0) x += w; else if (x > w) x -= w;
        if (y < 0) y += h; else if (y > h) y -= h;
    }
}