#version 150

//? if >= 26.3
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    fragColor = texture(Sampler0, texCoord) * vertexColor;
}
