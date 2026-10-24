#version 330

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

#ifdef NO_LAYOUT
in vec3 Position;
in vec2 UV0;
in vec4 RoundedParams0;
in vec4 RoundedParams1;

out vec2 texCoord;
out vec4 roundedParams0;
out vec4 roundedParams1;
#else
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 RoundedParams0;
layout(location = 3) in vec4 RoundedParams1;

layout(location = 0) out vec2 texCoord;
layout(location = 1) out vec4 roundedParams0;
layout(location = 2) out vec4 roundedParams1;
#endif

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = UV0;
    roundedParams0 = RoundedParams0;
    roundedParams1 = RoundedParams1;
}
