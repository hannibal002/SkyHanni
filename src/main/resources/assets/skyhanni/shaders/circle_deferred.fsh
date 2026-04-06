#version 150

const float pi = 3.14159265f;

in vec4 vertexColor;
in vec4 circleParams0;
in vec4 circleParams1;

out vec4 fragColor;

void main() {
    // circleParams0: {adjustedRadius, smoothness, angle1, angle2}
    // circleParams1: {adjustedCenterPosX, adjustedCenterPosY, unused, unused}
    // All values are pre-computed on the CPU, in physical pixels with y-flipped origin.
    float radius = circleParams0.x;
    float smoothness = circleParams0.y;
    float angle1 = circleParams0.z;
    float angle2 = circleParams0.w;
    vec2 centerPos = circleParams1.xy;

    vec2 adjusted = gl_FragCoord.xy - centerPos;

    float smoothed = 1.0 - smoothstep(pow(radius - smoothness, 2.0), pow(radius, 2.0), pow(adjusted.x, 2.0) + pow(adjusted.y, 2.0));

    float current = atan(adjusted.y, adjusted.x);
    float sanity = step(angle1, angle2);

    float lim1 = step(current, angle1);
    float lim2 = step(angle2, current);

    float lim3 = step(angle1, current);
    float lim4 = step(current, angle2);

    float lim = max(lim1, lim2) * sanity + (1.0 - sanity) * (1.0 - max(lim3, lim4));

    fragColor = vertexColor * vec4(1.0, 1.0, 1.0, smoothed * lim);
}
