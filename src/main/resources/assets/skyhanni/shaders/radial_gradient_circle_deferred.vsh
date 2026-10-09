#version 330

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

#ifdef NO_LAYOUT
in vec3 Position;
in vec4 RoundedParams0;
in vec4 RoundedParams1;
in vec4 GradientParams0;
in vec4 GradientParams1;
in vec4 GradientParams2;

out vec4 roundedParams0;
out vec4 roundedParams1;
out vec4 gradientParams0;
out vec4 gradientParams1;
out vec4 gradientParams2;
#else
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 RoundedParams0;
layout(location = 2) in vec4 RoundedParams1;
layout(location = 3) in vec4 GradientParams0;
layout(location = 4) in vec4 GradientParams1;
layout(location = 5) in vec4 GradientParams2;

layout(location = 0) out vec4 roundedParams0;
layout(location = 1) out vec4 roundedParams1;
layout(location = 2) out vec4 gradientParams0;
layout(location = 3) out vec4 gradientParams1;
layout(location = 4) out vec4 gradientParams2;
#endif

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    roundedParams0 = RoundedParams0;
    roundedParams1 = RoundedParams1;
    gradientParams0 = GradientParams0;
    gradientParams1 = GradientParams1;
    gradientParams2 = GradientParams2;
}
