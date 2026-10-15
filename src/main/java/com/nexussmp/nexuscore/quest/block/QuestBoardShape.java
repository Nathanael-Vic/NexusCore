package com.nexussmp.nexuscore.quest.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A forma do mural, fatiada por bloco.
 *
 * <p>O modelo tem 42 de largura por 29 de altura — quase tres blocos por dois. Como forma de colisao
 * so vale dentro do proprio bloco, o mural ocupa seis: o que a pessoa coloca (embaixo, no meio) e
 * cinco pedacos invisiveis em volta, cada um com a fatia da estrutura que cai nele.
 *
 * <p>As caixas aqui embaixo sao as do modelo, em coordenadas de modelo com o bloco colocado na
 * origem. O resto e recorte e giro: cada fatia e a intersecao com o cubo daquele bloco, virada para
 * o lado que o mural esta olhando.
 */
public final class QuestBoardShape {
    /** Quantos blocos o mural ocupa para cada lado do bloco colocado. */
    public static final int HALF_WIDTH = 1;
    /** Quantos blocos ele sobe acima do bloco colocado. */
    public static final int EXTRA_HEIGHT = 1;

    /** {x1, y1, z1, x2, y2, z2} em pixels de modelo, com o bloco colocado em 0..16. */
    private static final double[][] BOXES = {
            {-11, 9, 7, 27, 28, 9},   // a placa
            {-13, 0, 7, -11, 28, 9},  // poste da esquerda
            {27, 0, 7, 29, 28, 9},    // poste da direita
            {-13, 28, 5, 29, 29, 11}, // a tampa de cima
    };

    /**
     * As 24 fatias possiveis (4 direcoes x 3 colunas x 2 fiadas), prontas antes de alguem pedir.
     *
     * <p>Sao poucas e baratas de calcular, e a forma e pedida de dois lugares ao mesmo tempo: a
     * colisao roda na thread do servidor e o desenho na de render. Tabela pronta e imutavel nao tem
     * corrida possivel — um cache preenchido em tempo de execucao teria.
     */
    private static final VoxelShape[] SHAPES = shapes();

    /** A fatia que cabe no bloco de deslocamento {@code (cellX, cellY)}, para um mural virado assim. */
    public static VoxelShape of(Direction facing, int cellX, int cellY) {
        return SHAPES[index(facing, cellX, cellY)];
    }

    private static VoxelShape[] shapes() {
        VoxelShape[] shapes = new VoxelShape[Direction.Plane.HORIZONTAL.length() * 3 * 2];
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int cellX = -HALF_WIDTH; cellX <= HALF_WIDTH; cellX++) {
                for (int cellY = 0; cellY <= EXTRA_HEIGHT; cellY++) {
                    shapes[index(facing, cellX, cellY)] = build(facing, cellX, cellY);
                }
            }
        }
        return shapes;
    }

    private static int index(Direction facing, int cellX, int cellY) {
        // Direcao vertical nunca chega aqui (o bloco so tem facing horizontal), mas se chegasse,
        // get2DDataValue devolveria -1 e o indice sairia negativo.
        int horizontal = Math.max(0, facing.get2DDataValue());
        return (horizontal * 3 + (cellX + HALF_WIDTH)) * 2 + cellY;
    }

    private static VoxelShape build(Direction facing, int cellX, int cellY) {
        VoxelShape shape = Shapes.empty();
        for (double[] box : BOXES) {
            double x1 = Math.max(box[0], cellX * 16.0) - cellX * 16.0;
            double x2 = Math.min(box[3], cellX * 16.0 + 16.0) - cellX * 16.0;
            double y1 = Math.max(box[1], cellY * 16.0) - cellY * 16.0;
            double y2 = Math.min(box[4], cellY * 16.0 + 16.0) - cellY * 16.0;
            if (x2 <= x1 || y2 <= y1) {
                continue;
            }
            shape = Shapes.or(shape, rotate(facing, x1, y1, box[2], x2, y2, box[5]));
        }
        return shape.isEmpty() ? Shapes.empty() : shape;
    }

    /**
     * Gira a caixa junto com o modelo.
     *
     * <p>O blockstate vira o modelo em 90, 180 e 270 graus; a colisao tem de andar junto, senao a
     * parede fica de um lado e o empurrao do outro.
     */
    private static VoxelShape rotate(Direction facing, double x1, double y1, double z1,
                                     double x2, double y2, double z2) {
        return switch (facing) {
            case EAST -> Shapes.box((16 - z2) / 16, y1 / 16, x1 / 16, (16 - z1) / 16, y2 / 16, x2 / 16);
            case SOUTH -> Shapes.box((16 - x2) / 16, y1 / 16, (16 - z2) / 16, (16 - x1) / 16, y2 / 16, (16 - z1) / 16);
            case WEST -> Shapes.box(z1 / 16, y1 / 16, (16 - x2) / 16, z2 / 16, y2 / 16, (16 - x1) / 16);
            default -> Shapes.box(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
        };
    }

    /**
     * Onde fica o pedaco {@code (cellX, cellY)} de um mural colocado em {@code core}.
     *
     * <p>A largura do mural segue para os lados de quem olha para ele, entao o eixo muda com a
     * direcao: leste e oeste crescem no Z, norte e sul no X.
     */
    public static BlockPos offset(BlockPos core, Direction facing, int cellX, int cellY) {
        return switch (facing) {
            case EAST -> core.offset(0, cellY, cellX);
            case SOUTH -> core.offset(-cellX, cellY, 0);
            case WEST -> core.offset(0, cellY, -cellX);
            default -> core.offset(cellX, cellY, 0);
        };
    }

    private QuestBoardShape() {}
}
