#version 150

//? if >= 26.3
#extension GL_ARB_separate_shader_objects : require

in vec3 Position;
in vec2 UV0;
in vec4 Color;

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

out vec2 texCoord;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = UV0;
    vertexColor = Color;
}
