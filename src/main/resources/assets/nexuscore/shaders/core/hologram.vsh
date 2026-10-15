#version 150

// Vertice do holograma. Sem calculo de luz: a projecao brilha sozinha.
// "height" sai em altura de mundo (o uniforme traz a altura da camera), para as linhas de varredura
// ficarem presas ao mundo em vez de escorregarem quando o jogador pula.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 NexusHoloParams;

out float vertexDistance;
out vec4 vertexColor;
out vec4 overlayColor;
out vec2 texCoord0;
out float height;
out float rim;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
    vertexColor = Color;
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
    height = Position.y + NexusHoloParams.w;

    vec3 viewNormal = normalize((ModelViewMat * vec4(Normal, 0.0)).xyz);
    rim = 1.0 - abs(viewNormal.z);
}
