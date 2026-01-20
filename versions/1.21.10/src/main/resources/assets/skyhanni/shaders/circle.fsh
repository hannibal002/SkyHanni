#version 150

const float pi = 3.14159265f;

in vec4 vertexColor;

layout(std140) uniform SkyHanniRoundedUniforms {
    float scaleFactor;
    float radius;
    float smoothness;
    vec2 halfSize;
    vec2 centerPos;
    mat4 modelViewMatrix;
};

layout(std140) uniform SkyHanniCircleUniforms {
    float angle1;
    float angle2;
};

out vec4 fragColor;

void main() {
    float xScale = modelViewMatrix[0][0];
    float yScale = modelViewMatrix[1][1];
    float xTranslation = modelViewMatrix[3][0];
    float yTranslation = modelViewMatrix[3][1];

    vec2 cords = gl_FragCoord.xy;
    vec2 newCenterPos = vec2((centerPos.x + (radius * (xScale - 1.0))) + (xTranslation * scaleFactor), (centerPos.y - (radius * (yScale - 1.0))) - (yTranslation * scaleFactor));

    float newRadius = radius * min(xScale, yScale);
    vec2 adjusted = cords - newCenterPos;

    float smoothed = 1.0 - smoothstep(pow(newRadius - smoothness,2.0), pow(newRadius,2.0), pow(adjusted.x, 2.0) + pow(adjusted.y, 2.0));

    float current = atan(adjusted.y, adjusted.x);
    float sanity = step(angle1, angle2);

    float lim1 = step(current, angle1);
    float lim2 = step(angle2, current);

    float lim3 = step(angle1, current);
    float lim4 = step(current, angle2);

    float lim = max(lim1, lim2) * sanity + (1.0 - sanity) * (1.0 - max(lim3, lim4));

    fragColor = vertexColor * vec4(1.0, 1.0, 1.0, smoothed * lim);
}
