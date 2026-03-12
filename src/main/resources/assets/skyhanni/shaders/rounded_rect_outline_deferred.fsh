#version 150

in vec4 vertexColor;
in vec4 roundedParams0;
in vec4 roundedParams1;

out vec4 fragColor;

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
