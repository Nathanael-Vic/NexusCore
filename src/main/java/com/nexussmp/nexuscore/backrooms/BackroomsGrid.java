package com.nexussmp.nexuscore.backrooms;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A planta de um andar: onde e chao e onde e parede.
 *
 * <p>E a receita do gerador de mapas do davidpcahill/backrooms, que e a que produz aquela cara de
 * "Nivel 0": <b>centenas de labirintos parciais sobrepostos</b> em vez de um labirinto so. Cada
 * camada nasce num ponto qualquer, cresce pelo algoritmo de arvore crescente e fica sem orcamento
 * antes de virar um labirinto organizado; onde uma camada esbarra na outra, metade das vezes ela se
 * junta e metade das vezes morre ali. Sobrepostas, o que sai e o corredor sem fim e sem logica.
 *
 * <p>Depois disso entram as salas (retangulares, poligonais tortas e halls de pilares) e uma limpeza
 * que so tira ilha de chao presa dentro de parede. Parede solta no meio do chao fica: e pilar.
 *
 * <p>So contas e memoria, sem tocar no mundo — quem poe bloco e o {@link BackroomsBuilder}.
 */
public final class BackroomsGrid {
    public static final byte WALL = 0;
    public static final byte FLOOR = 1;

    /** Para de escavar corredor quando esta fracao do andar ja e chao. */
    private static final float FILL = 0.55F;
    private static final int MIN_BUDGET = 30;
    private static final int MAX_BUDGET = 300;
    /** Chance de uma camada morrer ao encostar em corredor de outra (o resto se junta). */
    private static final float MERGE_STOP = 0.5F;
    /** Espessura da parede externa: o andar e fechado, ninguem cai no vazio. */
    private static final int BORDER = 2;

    // Quantidades do gerador original, que trabalhava num mapa de 160x90 celulas. Aqui elas crescem
    // junto com a area: um andar grande nao pode ter tres salas perdidas no meio do corredor.
    private static final int REFERENCE_CELLS = 160 * 90;
    private static final int RECT_ROOMS = 3;
    private static final int PILLAR_ROOMS = 2;
    private static final int POLY_ROOMS = 2;

    private final int size;
    private final byte[] cells;
    /** Quantas celulas ja viraram chao: e o que diz quando os corredores bastam. */
    private int floorCount;

    private BackroomsGrid(int size) {
        this.size = size;
        this.cells = new byte[size * size];
    }

    public static BackroomsGrid generate(int size, long seed) {
        BackroomsGrid grid = new BackroomsGrid(size);
        RandomSource random = RandomSource.create(seed);
        grid.carveCorridors(random);
        grid.carveRectRooms(random);
        grid.carvePolyRooms(random);
        // Os halls de pilares vao por ultimo: assim nada carimba por cima dos pilares deles.
        grid.carvePillarRooms(random);
        grid.cleanup();
        grid.sealBorder();
        return grid;
    }

    public int size() {
        return size;
    }

    public boolean isFloor(int x, int z) {
        return inside(x, z) && cells[z * size + x] == FLOOR;
    }

    private boolean inside(int x, int z) {
        return x >= 0 && z >= 0 && x < size && z < size;
    }

    private void setFloor(int index) {
        if (cells[index] == WALL) {
            cells[index] = FLOOR;
            floorCount++;
        }
    }

    private void setWall(int index) {
        if (cells[index] == FLOOR) {
            cells[index] = WALL;
            floorCount--;
        }
    }

    // ---- corredores ----

    private void carveCorridors(RandomSource random) {
        int target = (int) (cells.length * FILL);
        // Cada camada escava no maximo MAX_BUDGET celulas; o teto de camadas e so valvula de seguranca.
        int maxLayers = Math.max(4000, cells.length / 25);
        int[] visited = new int[cells.length];
        IntArrayList active = new IntArrayList();
        for (int layer = 1; layer <= maxLayers && floorCount < target; layer++) {
            carveMazeLayer(random, visited, layer, active);
        }
    }

    /**
     * Uma camada de labirinto parcial.
     *
     * <p>As celulas do labirinto ficam de duas em duas; a parede entre duas vira chao quando a
     * passagem abre. O orcamento limitado e o que impede uma camada de dominar o andar.
     */
    private void carveMazeLayer(RandomSource random, int[] visited, int layer, IntArrayList active) {
        active.clear();
        int lattice = Math.max(1, (size - 1) / 2);
        int start = random.nextInt(lattice) * 2 * size + random.nextInt(lattice) * 2;
        active.add(start);
        visited[start] = layer;
        int budget = Mth.nextInt(random, MIN_BUDGET, MAX_BUDGET);

        while (!active.isEmpty() && budget > 0) {
            int pick = random.nextInt(active.size());
            int cell = active.getInt(pick);
            int x = cell % size;
            int z = cell / size;
            setFloor(cell);

            int chosen = -1;
            int candidates = 0;
            for (int direction = 0; direction < 4; direction++) {
                int nx = x + (direction == 0 ? -2 : direction == 1 ? 2 : 0);
                int nz = z + (direction == 2 ? -2 : direction == 3 ? 2 : 0);
                if (!inside(nx, nz)) {
                    continue;
                }
                int neighbor = nz * size + nx;
                if (visited[neighbor] == layer) {
                    continue;
                }
                // Sorteio uniforme entre os vizinhos validos sem montar lista.
                if (random.nextInt(++candidates) == 0) {
                    chosen = neighbor;
                }
            }
            if (chosen < 0) {
                active.removeInt(pick);
                continue;
            }

            visited[chosen] = layer;
            // Esbarrou em corredor de outra camada: metade das vezes morre aqui, metade se junta.
            if (cells[chosen] == FLOOR && random.nextFloat() < MERGE_STOP) {
                continue;
            }
            setFloor((z + chosen / size) / 2 * size + (x + chosen % size) / 2);
            setFloor(chosen);
            active.add(chosen);
            budget--;
        }
    }

    // ---- salas ----

    private int scaled(int reference) {
        return Math.max(reference, Math.round((float) reference * cells.length / REFERENCE_CELLS));
    }

    private void carveRectRooms(RandomSource random) {
        for (int i = 0; i < scaled(RECT_ROOMS); i++) {
            int width = Math.min(Mth.nextInt(random, 4, 32), size - 2);
            int depth = Math.min(Mth.nextInt(random, 4, 32), size - 2);
            stamp(random.nextInt(size - width - 1) + 1, random.nextInt(size - depth - 1) + 1, width, depth);
        }
    }

    private void carvePillarRooms(RandomSource random) {
        for (int i = 0; i < scaled(PILLAR_ROOMS); i++) {
            int width = Math.min(Mth.nextInt(random, 10, 36), size - 2);
            int depth = Math.min(Mth.nextInt(random, 10, 36), size - 2);
            int x = random.nextInt(size - width - 1) + 1;
            int z = random.nextInt(size - depth - 1) + 1;
            stamp(x, z, width, depth);

            int spacing = Mth.nextInt(random, 3, 6);
            // Pilares recuados: o hall mantem um corredor livre em volta.
            for (int pz = z + spacing; pz < z + depth - 1; pz += spacing) {
                for (int px = x + spacing; px < x + width - 1; px += spacing) {
                    setWall(pz * size + px);
                }
            }
        }
    }

    /** Camaras tortas: angulos e raios sorteados, para nao sairem poligonos certinhos. */
    private void carvePolyRooms(RandomSource random) {
        for (int i = 0; i < scaled(POLY_ROOMS); i++) {
            int sides = Mth.nextInt(random, 3, 9);
            int radius = Math.min(Mth.nextInt(random, 4, 18), (size - 4) / 2);
            int cx = random.nextInt(size - 2 * radius - 2) + radius + 1;
            int cz = random.nextInt(size - 2 * radius - 2) + radius + 1;

            double step = Math.PI * 2.0 / sides;
            double[] vertexX = new double[sides];
            double[] vertexZ = new double[sides];
            for (int vertex = 0; vertex < sides; vertex++) {
                double angle = vertex * step + (random.nextDouble() - 0.5) * step / 2.0;
                double r = radius * (0.55 + random.nextDouble() * 0.45);
                vertexX[vertex] = cx + r * Math.cos(angle);
                vertexZ[vertex] = cz + r * Math.sin(angle);
            }
            for (int z = cz - radius; z <= cz + radius; z++) {
                for (int x = cx - radius; x <= cx + radius; x++) {
                    if (inside(x, z) && insidePolygon(x + 0.5, z + 0.5, vertexX, vertexZ)) {
                        setFloor(z * size + x);
                    }
                }
            }
        }
    }

    private static boolean insidePolygon(double x, double z, double[] vertexX, double[] vertexZ) {
        boolean inside = false;
        for (int i = 0, j = vertexX.length - 1; i < vertexX.length; j = i++) {
            boolean crosses = (vertexZ[i] > z) != (vertexZ[j] > z);
            if (crosses && x < (vertexX[j] - vertexX[i]) * (z - vertexZ[i]) / (vertexZ[j] - vertexZ[i]) + vertexX[i]) {
                inside = !inside;
            }
        }
        return inside;
    }

    private void stamp(int x, int z, int width, int depth) {
        for (int row = z; row < z + depth; row++) {
            for (int col = x; col < x + width; col++) {
                if (inside(col, row)) {
                    setFloor(row * size + col);
                }
            }
        }
    }

    // ---- acabamento ----

    /** Tira ilha de chao presa dentro de parede. Parede solta no meio do chao fica: e pilar. */
    private void cleanup() {
        IntArrayList orphans = new IntArrayList();
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = z * size + x;
                if (cells[index] != FLOOR) {
                    continue;
                }
                boolean alone = true;
                for (int dz = -1; dz <= 1 && alone; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if ((dx != 0 || dz != 0) && isFloor(x + dx, z + dz)) {
                            alone = false;
                            break;
                        }
                    }
                }
                if (alone) {
                    orphans.add(index);
                }
            }
        }
        for (int index : orphans) {
            setWall(index);
        }
    }

    private void sealBorder() {
        for (int i = 0; i < size; i++) {
            for (int b = 0; b < BORDER; b++) {
                setWall(b * size + i);
                setWall((size - 1 - b) * size + i);
                setWall(i * size + b);
                setWall(i * size + size - 1 - b);
            }
        }
    }
}
