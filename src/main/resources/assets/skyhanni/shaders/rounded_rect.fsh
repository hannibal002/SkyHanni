// Based on https://www.shadertoy.com/view/WtdSDs

#version 130

uniform float radius;
uniform float smoothness;
uniform vec2 halfSize;
uniform vec2 centerPos;

varying vec4 color;

float roundedRectSDF(vec2 center, vec2 halfSize, float radius) {
    return length(max(abs(center) - halfSize + radius, 0.0)) - radius;
}

void main() {
    float distance = roundedRectSDF(gl_FragCoord.xy - centerPos, halfSize, radius);
    float smoothed = 1.0 - smoothstep(0.0, smoothness, distance);
    gl_FragColor = color * vec4(1.0, 1.0, 1.0, smoothed);
}