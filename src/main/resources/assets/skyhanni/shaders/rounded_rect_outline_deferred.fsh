#version 330

//? if >= 26.3
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec4 roundedParams0;
layout(location = 2) in vec4 roundedParams1;

layout(location = 0) out vec4 fragColor;

float roundedRectSDF(vec2 center, vec2 halfSize, float radius) {
    return length(max(abs(center) - halfSize + radius, 0.0)) - radius;
}

void main() {
    float radius = roundedParams0.x;
    float borderThickness = roundedParams0.y;
    vec2 halfSize = roundedParams0.zw;
    vec2 centerPos = roundedParams1.xy;
    float borderBlur = roundedParams1.z;

    float distance = roundedRectSDF(gl_FragCoord.xy - centerPos, halfSize, max(radius, borderThickness));
    float smoothed = 1.0 - smoothstep(borderBlur, 1.0, abs(distance / borderThickness));
    fragColor = vertexColor * vec4(1.0, 1.0, 1.0, smoothed);
}
