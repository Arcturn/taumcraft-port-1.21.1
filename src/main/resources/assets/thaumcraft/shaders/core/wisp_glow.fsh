#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D OpaqueDepth;
uniform sampler2D TranslucentColor;
uniform sampler2D TranslucentDepth;
uniform sampler2D ItemColor;
uniform sampler2D ItemDepth;
uniform sampler2D ParticlesColor;
uniform sampler2D ParticlesDepth;
uniform sampler2D WeatherColor;
uniform sampler2D WeatherDepth;
uniform sampler2D CloudsColor;
uniform sampler2D CloudsDepth;
uniform int UseTransparencyLayers;
uniform int UseClouds;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float transmission(sampler2D color, sampler2D depth, ivec2 pixel) {
    // Match the same nearest layer depths used by vanilla's Fabulous compositor.
    // Background water/clouds contribute no attenuation to a foreground glow.
    return texelFetch(depth, pixel, 0).r < gl_FragCoord.z
        ? 1.0 - texelFetch(color, pixel, 0).a : 1.0;
}

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator
        * linear_fog_fade(vertexDistance, FogStart, FogEnd);
    if (UseTransparencyLayers != 0) {
        ivec2 pixel = ivec2(gl_FragCoord.xy);
        if (texelFetch(OpaqueDepth, pixel, 0).r < gl_FragCoord.z) discard;
        float visibility = transmission(TranslucentColor, TranslucentDepth, pixel)
            * transmission(ItemColor, ItemDepth, pixel)
            * transmission(ParticlesColor, ParticlesDepth, pixel)
            * transmission(WeatherColor, WeatherDepth, pixel);
        if (UseClouds != 0) visibility *= transmission(CloudsColor, CloudsDepth, pixel);
        // SRC_ALPHA,ONE already premultiplies RGB by alpha. Apply transmission once.
        color.a *= visibility;
    }
    fragColor = color;
}
