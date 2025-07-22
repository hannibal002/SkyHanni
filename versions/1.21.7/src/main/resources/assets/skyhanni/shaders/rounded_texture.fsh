#version 150

in vec2 texCoord;

layout(std140) uniform SkyHanniRoundedUniforms {
    float scaleFactor;
    float radius;
    float smoothness;
    vec2 halfSize;
    vec2 centerPos;
    mat4 modelViewMatrix;
};

uniform sampler2D textureSampler;

out vec4 outColor;

// From https://www.shadertoy.com/view/WtdSDs
float roundedRectSDF(vec2 center, vec2 halfSize, float radius) {
    return length(max(abs(center) - halfSize + radius, 0.0)) - radius;
}

void main() {
    float xScale = modelViewMatrix[0][0];
    float yScale = modelViewMatrix[1][1];
    float xTranslation = modelViewMatrix[3][0];
    float yTranslation = modelViewMatrix[3][1];

    vec2 newHalfSize = vec2(halfSize.x * xScale, halfSize.y * yScale);

    float newCenterPosY = centerPos.y;
    if (yScale != 1.0) {
        newCenterPosY = centerPos.y - (halfSize.y * (yScale - 1));
    }

    vec2 newCenterPos = vec2((centerPos.x * xScale) + (xTranslation * scaleFactor), newCenterPosY - (yTranslation * scaleFactor));

    float distance = roundedRectSDF(gl_FragCoord.xy - newCenterPos, newHalfSize, radius);
    float smoothed = 1.0 - smoothstep(0.0, smoothness, distance);
    outColor = texture(textureSampler, texCoord) * vec4(1.0, 1.0, 1.0, smoothed);
}
