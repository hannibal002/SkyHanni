#version 330

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

#ifdef NO_LAYOUT
in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord;
out vec4 vertexColor;
#else
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec2 texCoord;
layout(location = 1) out vec4 vertexColor;
#endif

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = UV0;
    vertexColor = Color;
}
