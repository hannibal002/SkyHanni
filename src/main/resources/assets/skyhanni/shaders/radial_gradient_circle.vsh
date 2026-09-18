#version 150

//? if >= 26.3
#extension GL_ARB_separate_shader_objects : require

in vec3 Position;

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
