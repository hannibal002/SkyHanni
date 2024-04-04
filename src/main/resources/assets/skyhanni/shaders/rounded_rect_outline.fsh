#version 120

uniform float scaleFactor;
uniform float radius;
uniform float outlineWidth;
uniform vec2 halfSize;
uniform vec2 centerPos;

varying vec4 color;

// From https://www.shadertoy.com/view/WtdSDs
float roundedRectSDF(vec2 center, vec2 halfSize, float radius) {
    return length(max(abs(center) - halfSize + radius, 0.0)) - radius;
}

void main() {
    float xScale = gl_ModelViewMatrix[0][0];
    float yScale = gl_ModelViewMatrix[1][1];
    float xTranslation = gl_ModelViewMatrix[3][0];
    float yTranslation = gl_ModelViewMatrix[3][1];

    vec2 newHalfSize = vec2(halfSize.x * xScale, halfSize.y * yScale);

    float newCenterPosY = centerPos.y;
    if (yScale > 1.0) {
        newCenterPosY = centerPos.y - (halfSize.y * (yScale - 1));
    }

    vec2 newCenterPos = vec2((centerPos.x * xScale) + (xTranslation * scaleFactor), newCenterPosY - (yTranslation * scaleFactor));

    float distance = roundedRectSDF(gl_FragCoord.xy - newCenterPos, newHalfSize, radius);
    float outline = abs(distance) - outlineWidth / scaleFactor;
    float smoothedOutline = 1.0 - smoothstep(0.0, 1.0, outline);

    // Inside the rounded rectangle, but not inside the outline
    if (distance < 0.0 && outline > 0.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0); // Transparent
    }
    // Inside the outline
    else if (outline < 0.0) {
        gl_FragColor = color * vec4(1.0, 1.0, 1.0, smoothedOutline);
    }
    // Outside the rounded rectangle
    else {
        gl_FragColor = color * vec4(1.0, 1.0, 1.0, 0.0); // Transparent
    }
}
