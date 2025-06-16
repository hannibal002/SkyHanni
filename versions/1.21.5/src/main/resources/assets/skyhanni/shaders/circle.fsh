#version 150

uniform float scaleFactor;
uniform float radius;
uniform float smoothness;
uniform vec2 centerPos;
uniform float angle1;
uniform float angle2;

in vec4 vertexColor;
out vec4 fragColor;

const float pi = 3.1415926535897932384626433832795028841971693993751058209749445923078164062;

void main() {
    vec2 fragCoord = gl_FragCoord.xy;
    vec2 adjusted = fragCoord - centerPos;

    float dist2 = dot(adjusted, adjusted);
    float smoothed = 1.0 - smoothstep(
    (radius - smoothness) * (radius - smoothness),
    radius * radius,
    dist2
    );

    float current = atan(adjusted.y, adjusted.x);

    float sanity = step(angle1, angle2);

    float lim1 = step(current, angle1);
    float lim2 = step(angle2, current);

    float lim3 = step(angle1, current);
    float lim4 = step(current, angle2);

    float lim = max(lim1, lim2) * sanity + (1.0 - sanity) * (1.0 - max(lim3, lim4));

    fragColor = vertexColor * vec4(1.0, 1.0, 1.0, smoothed * lim);
}
