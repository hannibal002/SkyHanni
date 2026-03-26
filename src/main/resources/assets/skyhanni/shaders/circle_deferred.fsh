#version 150

const float pi = 3.14159265f;

in vec4 vertexColor;
in vec4 roundedParams0;
in vec4 roundedParams1;

out vec4 fragColor;

void main() {
    float smoothness = roundedParams0.y;
    vec2 halfSize = roundedParams0.zw;
    vec2 centerPos = roundedParams1.xy;
    float angle1 = roundedParams1.z;
    float angle2 = roundedParams1.w;

    float radius = min(halfSize.x, halfSize.y);

    vec2 adjusted = gl_FragCoord.xy - centerPos;
    float dist2 = dot(adjusted, adjusted);
    float smoothed = 1.0 - smoothstep(pow(radius - smoothness, 2.0), pow(radius, 2.0), dist2);

    float current = atan(adjusted.y, adjusted.x);
    float sanity = step(angle1, angle2);

    float lim1 = step(current, angle1);
    float lim2 = step(angle2, current);

    float lim3 = step(angle1, current);
    float lim4 = step(current, angle2);

    float lim = max(lim1, lim2) * sanity + (1.0 - sanity) * (1.0 - max(lim3, lim4));

    fragColor = vertexColor * vec4(1.0, 1.0, 1.0, smoothed * lim);
}
