package asteroids;

public class Tiro {
    public double x, y, vx, vy;
    public double vida = 1.2; // segundos

    public Tiro(double x, double y, double vx, double vy) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
    }

    public void mover(double dt, int w, int h) {
        x += vx * dt; y += vy * dt; vida -= dt;
        if (x < 0) x += w; else if (x > w) x -= w;
        if (y < 0) y += h; else if (y > h) y -= h;
    }
}