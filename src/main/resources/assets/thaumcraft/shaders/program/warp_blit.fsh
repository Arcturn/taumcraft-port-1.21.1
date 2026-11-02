#version 150

out vec4 fragColor;

uniform sampler2D DiffuseSampler;

uniform vec4 ColorModulate;

in vec2 texCoord;

void main(){
    fragColor = texture(DiffuseSampler, texCoord) * ColorModulate;
}
