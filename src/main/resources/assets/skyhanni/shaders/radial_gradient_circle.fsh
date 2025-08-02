#version 120

const float pi = 3.14159265;
const float tau = 6.2831853;

uniform float scaleFactor;
uniform float radius;
uniform float smoothness;
uniform vec2 centerPos;

uniform float angle;
uniform vec4 startColor;
uniform vec4 endColor;
uniform float progress;
uniform float phaseOffset;
uniform int reverse;

void main() {
    float xScale = gl_ModelViewMatrix[0][0];
    float yScale = gl_ModelViewMatrix[1][1];
    float xTranslation = gl_ModelViewMatrix[3][0];
    float yTranslation = gl_ModelViewMatrix[3][1];

    vec2 cords = gl_FragCoord.xy;
    vec2 newCenterPos = vec2((centerPos.x + (radius * (xScale - 1.0))) + (xTranslation * scaleFactor), (centerPos.y - (radius * (yScale - 1.0))) - (yTranslation * scaleFactor));
    vec2 adjusted = cords - newCenterPos;

    float newRadius = radius * min(xScale, yScale);
    float smoothed = 1.0 - smoothstep(pow(newRadius - smoothness,2.0), pow(newRadius,2.0), pow(adjusted.x, 2.0) + pow(adjusted.y, 2.0));
    if (smoothed <= 0.0) discard;

    float intAngle = atan(adjusted.y, adjusted.x);
    intAngle = mod(intAngle + tau, tau);

    float angleOffset = mod(intAngle - angle + tau, tau);
    float angularLength = progress * tau;

    float angleSoft = smoothness / radius;
    float angleAlpha = 1.0 - smoothstep(angularLength - angleSoft, angularLength, angleOffset);

    float finalAlpha = smoothed * angleAlpha;
    if (finalAlpha <= 0.0) discard;

    float factor = fract(angleOffset / tau + phaseOffset);
    if (reverse == 1) factor = 1.0 - factor;

    vec4 color = mix(startColor, endColor, factor);
    gl_FragColor = vec4(color.rgb, color.a * finalAlpha);
}
