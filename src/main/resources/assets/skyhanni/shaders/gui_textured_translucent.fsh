#version 330

#ifdef NO_LAYOUT
in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;
#else
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;
#endif

uniform sampler2D Sampler0;

void main() {
    fragColor = texture(Sampler0, texCoord) * vertexColor;
}
