#version 330

//? if >= 26.3
#extension GL_ARB_separate_shader_objects : require

const float tau = 6.2831853f;

layout(location = 0) in vec4 roundedParams0;
layout(location = 1) in vec4 roundedParams1;
layout(location = 2) in vec4 gradientParams0;
layout(location = 3) in vec4 gradientParams1;
layout(location = 4) in vec4 gradientParams2;

layout(location = 0) out vec4 fragColor;

void main() {
    float smoothness = roundedParams0.y;
    vec2 halfSize = roundedParams0.zw;
    vec2 centerPos = roundedParams1.xy;

    float angle = gradientParams0.x;
    float progress = gradientParams0.y;
    float phaseOffset = gradientParams0.z;
    bool reverse = gradientParams0.w > 0.5;

    vec4 startColor = gradientParams1;
    vec4 endColor = gradientParams2;

    float radius = min(halfSize.x, halfSize.y);

    vec2 adjusted = gl_FragCoord.xy - centerPos;
    float smoothed = 1.0 - smoothstep(pow(radius - smoothness, 2.0), pow(radius, 2.0), dot(adjusted, adjusted));
    if (smoothed <= 0.0) discard;

    float intAngle = atan(adjusted.y, adjusted.x);
    intAngle = mod(intAngle + tau, tau);

    float angularLength = progress * tau;
    float angleSoft = smoothness / radius;
    float angleOffset = mod(intAngle - angle + tau, tau);
    float angleAlpha = 1.0 - smoothstep(angularLength - angleSoft, angularLength, angleOffset);

    float finalAlpha = smoothed * angleAlpha;
    if (finalAlpha <= 0.0) discard;

    float factor = fract(angleOffset / tau + phaseOffset);
    if (reverse) factor = 1.0 - factor;

    vec4 color = mix(startColor, endColor, factor);
    fragColor = vec4(color.rgb, color.a * finalAlpha);
}
