#version 330

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

#ifdef NO_LAYOUT
in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec4 vertexColor;
out vec2 texCoord0;
#else
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;
#endif

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // Pass the color & texture coords to the fragment shader
    vertexColor = Color;
    texCoord0 = UV0;
}
