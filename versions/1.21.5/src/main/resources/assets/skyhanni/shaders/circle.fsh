#version 150 core

const float pi = 3.14159265;

uniform float scaleFactor;
uniform float radius;
uniform float smoothness;
uniform vec2 centerPos;
uniform float angle1;
uniform float angle2;
uniform mat4 modelViewMatrix;

in vec4 color;
out vec4 fragColor;

void main() {
    float xScale = modelViewMatrix[0][0];
    float yScale = modelViewMatrix[1][1];
    float xTranslation = modelViewMatrix[3][0];
    float yTranslation = modelViewMatrix[3][1];

    vec2 cords = gl_FragCoord.xy;

    vec2 newCenterPos = vec2(
        (centerPos.x + radius * (xScale - 1.0)) + xTranslation * scaleFactor,
        (centerPos.y - radius * (yScale - 1.0)) - yTranslation * scaleFactor
    );

    float newRadius = radius * min(xScale, yScale);

    vec2 adjusted = cords - newCenterPos;

    float smoothed = 1.0 - smoothstep(
        pow(newRadius - smoothness, 2.0),
        pow(newRadius, 2.0),
        adjusted.x * adjusted.x + adjusted.y * adjusted.y
    );

    float current = atan(adjusted.y, adjusted.x);

    float sanity = step(angle1, angle2);

    float lim1 = step(current, angle1);
    float lim2 = step(angle2, current);
    float lim3 = step(angle1, current);
    float lim4 = step(current, angle2);

    float lim = max(lim1, lim2) * sanity
    + (1.0 - sanity) * (1.0 - max(lim3, lim4));

    fragColor = color * vec4(1.0, 1.0, 1.0, smoothed * lim);
}
