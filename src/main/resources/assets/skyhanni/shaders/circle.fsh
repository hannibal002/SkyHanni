#version 120

uniform float scaleFactor;
uniform float radius;
uniform float smoothness;
uniform vec2 centerPos;

varying vec4 color;

void main() {
    float xScale = gl_ModelViewMatrix[0][0];
    float yScale = gl_ModelViewMatrix[1][1];
    float xTranslation = gl_ModelViewMatrix[3][0];
    float yTranslation = gl_ModelViewMatrix[3][1];

    vec2 cords = vec2(gl_FragCoord.x, gl_FragCoord.y);

    vec2 newCenterPos = vec2((centerPos.x + (radius * (xScale - 1.0))) + (xTranslation * scaleFactor), (centerPos.y - (radius * (yScale - 1.0))) - (yTranslation * scaleFactor));

    float newRadius = pow(radius * min(xScale,yScale),2.0);

    vec2 adjusted = cords - newCenterPos;

    float smoothed = 1.0 - smoothstep(newRadius - smoothness, newRadius, pow(adjusted.x, 2.0) + pow(adjusted.y, 2.0));
    gl_FragColor = color * vec4(1.0, 1.0, 1.0, smoothed);
}
