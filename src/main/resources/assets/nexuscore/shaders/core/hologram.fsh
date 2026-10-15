#version 150

#moj_import <fog.glsl>

// Fragmento do holograma: a textura da skin vira uma cor so (a do holograma), com linhas de
// varredura subindo, oscilacao, falhas ocasionais e a silhueta mais acesa.

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float GameTime;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 NexusHoloColor;   // rgb: cor da projecao | a: forca do brilho da silhueta
uniform vec4 NexusHoloParams;  // x: transparencia | y: linhas por bloco | z: oscilacao | w: altura da camera

in float vertexDistance;
in vec4 vertexColor;
in vec4 overlayColor;
in vec2 texCoord0;
in float height;
in float rim;

out vec4 fragColor;

float hash(float value) {
    return fract(sin(value) * 43758.5453123);
}

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.1) {
        discard;
    }
    vec4 color = texel * vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);

    float time = GameTime * 24000.0;

    float lines = NexusHoloParams.y > 0.0
        ? 0.5 + 0.5 * sin((height * NexusHoloParams.y - time * 0.35) * 6.2831853)
        : 1.0;
    float scan = mix(0.55, 1.0, lines);

    float band = step(0.9965, hash(floor(height * 8.0) + floor(time * 0.6)));
    float flicker = 1.0 - NexusHoloParams.z * (0.5 + 0.5 * sin(time * 1.7) * sin(time * 0.53));

    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 tint = NexusHoloColor.rgb * (0.30 + 0.95 * luma);
    tint += NexusHoloColor.rgb * pow(rim, 2.5) * NexusHoloColor.a;

    float alpha = color.a * NexusHoloParams.x * scan * flicker * (1.0 - band * 0.85);
    if (alpha < 0.02) {
        discard;
    }
    fragColor = vec4(tint, alpha) * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
