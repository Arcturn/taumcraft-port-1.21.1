#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform float Density;
uniform float ProjectionA;
uniform float ProjectionB;
uniform float FogRed;
uniform float FogGreen;
uniform float FogBlue;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r * 2.0 - 1.0;
    float eyeZ = abs(ProjectionB / (depth + ProjectionA));
    float visibility = exp(-Density * eyeZ);
    fragColor = vec4(mix(vec3(FogRed, FogGreen, FogBlue), color.rgb, visibility), color.a);
}
