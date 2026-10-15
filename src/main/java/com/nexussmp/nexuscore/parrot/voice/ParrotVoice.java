package com.nexussmp.nexuscore.parrot.voice;

/**
 * A voz de papagaio: mais fina, sem virar disco acelerado.
 *
 * <p>Tocar a voz mais depressa deixa ela fina, mas tambem corrida — e e isso que faz soar como
 * esquilo, nao como papagaio. Aqui sao dois passos:
 * <ol>
 *   <li><b>Afinar:</b> a onda e tocada {@code pitch} vezes mais depressa. Sobe o tom e o timbre (a
 *       "boca" fica menor, que e o jeito de papagaio), mas encurta a fala na mesma proporcao.</li>
 *   <li><b>Devolver o tempo:</b> a fala encurtada e esticada de volta sem mexer no tom, pelo
 *       metodo WSOLA: ela e remontada em pedacos de 40 ms sobrepostos pela metade, e cada pedaco
 *       e escolhido, numa janela pequena em volta de onde deveria estar, no ponto em que a onda
 *       mais se parece com a continuacao do anterior. Sem essa escolha os pedacos se cancelam e a
 *       voz sai metalica; com ela, sai lisa.</li>
 * </ol>
 * No fim a fala fica so {@code speed} vezes mais rapida — um tiquinho, que papagaio fala ligeiro.
 *
 * <p>Codigo Java puro, sem nada do chat de voz: entra e sai onda de 48 kHz.
 */
final class ParrotVoice {
    /** Tamanho de cada pedaco: 40 ms, o bastante para conter alguns ciclos da voz mais grave. */
    private static final int WINDOW = 1920;
    /** Passo de montagem: metade do pedaco, e a janela de Hann soma exatamente 1 assim. */
    private static final int HOP = WINDOW / 2;
    /** Quanto cada pedaco pode escorregar para achar o encaixe: 10 ms para cada lado. */
    private static final int TOLERANCE = 480;
    /** Trecho comparado no encaixe, e de quantas em quantas amostras (a conta fica 4x mais leve). */
    private static final int MATCH_LENGTH = 960;
    private static final int MATCH_STEP = 4;
    /** A primeira passada do encaixe pula de tantas em tantas amostras. */
    private static final int COARSE_STEP = 4;

    private static final float[] HANN = hann(WINDOW);

    /** A voz afinada {@code pitch} vezes e tocada {@code speed} vezes mais depressa. */
    static short[] transform(short[] samples, double pitch, double speed) {
        if (samples.length == 0) {
            return samples;
        }
        float[] raised = resample(samples, pitch);
        float[] timed = stretch(raised, pitch / speed);
        short[] out = new short[timed.length];
        for (int i = 0; i < timed.length; i++) {
            out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(timed[i])));
        }
        return out;
    }

    /** Toca a onda {@code rate} vezes mais depressa (interpolando entre as amostras). */
    private static float[] resample(short[] samples, double rate) {
        int length = Math.max(1, (int) (samples.length / rate));
        float[] out = new float[length];
        for (int i = 0; i < length; i++) {
            double source = i * rate;
            int index = (int) source;
            double blend = source - index;
            int first = samples[Math.min(index, samples.length - 1)];
            int second = samples[Math.min(index + 1, samples.length - 1)];
            out[i] = (float) (first + (second - first) * blend);
        }
        return out;
    }

    /**
     * Estica a onda {@code factor} vezes no tempo sem mexer no tom (WSOLA).
     *
     * <p>Fala curta demais para ter dois pedacos fica como esta: e meio segundo de som, e a diferenca
     * de velocidade nao se nota.
     */
    private static float[] stretch(float[] input, double factor) {
        if (factor == 1.0 || input.length < WINDOW * 2) {
            return input;
        }
        int outLength = (int) (input.length * factor);
        float[] out = new float[outLength + WINDOW];
        double analysisHop = HOP / factor;
        int frames = (outLength - WINDOW) / HOP + 1;

        int previous = 0;
        for (int frame = 0; frame < frames; frame++) {
            int nominal = (int) Math.round(frame * analysisHop);
            int position = frame == 0 ? 0 : bestMatch(input, previous + HOP, nominal);
            int outStart = frame * HOP;
            for (int i = 0; i < WINDOW; i++) {
                int source = position + i;
                if (source >= input.length) {
                    break;
                }
                out[outStart + i] += input[source] * HANN[i];
            }
            previous = position;
        }
        // O primeiro meio pedaco so recebeu uma janela, que comeca do zero: completa com a original
        // para a fala nao comecar abafada.
        for (int i = 0; i < HOP && i < input.length; i++) {
            out[i] = input[i];
        }
        return java.util.Arrays.copyOf(out, outLength);
    }

    /**
     * Onde, perto de {@code nominal}, a onda mais se parece com a continuacao natural do pedaco
     * anterior (o que vem logo depois dele na original, em {@code natural}).
     */
    private static int bestMatch(float[] input, int natural, int nominal) {
        int limit = input.length - WINDOW;
        if (limit <= 0) {
            return 0;
        }
        int from = Math.max(0, nominal - TOLERANCE);
        int to = Math.min(limit, nominal + TOLERANCE);
        int best = Math.min(Math.max(nominal, 0), limit);
        if (natural + MATCH_LENGTH > input.length || from > to) {
            return best;
        }
        // Primeiro por cima, de quatro em quatro; depois o acerto fino em volta do melhor.
        int coarse = search(input, natural, from, to, COARSE_STEP, MATCH_STEP * 2, best);
        return search(input, natural, Math.max(from, coarse - COARSE_STEP + 1),
                Math.min(to, coarse + COARSE_STEP - 1), 1, MATCH_STEP, coarse);
    }

    private static int search(float[] input, int natural, int from, int to, int step, int matchStep, int fallback) {
        int best = fallback;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int candidate = from; candidate <= to; candidate += step) {
            double score = 0.0;
            for (int i = 0; i < MATCH_LENGTH; i += matchStep) {
                score += input[candidate + i] * input[natural + i];
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /** Janela de Hann periodica: com passo de meia janela, as sobreposicoes somam exatamente 1. */
    private static float[] hann(int size) {
        float[] window = new float[size];
        for (int i = 0; i < size; i++) {
            window[i] = (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / size));
        }
        return window;
    }

    private ParrotVoice() {}
}
