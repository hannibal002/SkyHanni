#version 330

//? if >= 26.3
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec4 RoundedParams0;
layout(location = 3) in vec4 RoundedParams1;

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec4 roundedParams0;
layout(location = 2) out vec4 roundedParams1;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    roundedParams0 = RoundedParams0;
    roundedParams1 = RoundedParams1;
}
