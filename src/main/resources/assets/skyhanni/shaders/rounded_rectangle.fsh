#version 330 core

uniform vec2 size;
uniform float radius;
uniform vec4 color;

void main() {
    vec2 position = gl_FragCoord.xy;
    vec2 halfSize = size * 0.5;

    // Calculate distance to the closest point on the rectangle
    vec2 dist = abs(position - halfSize);

    // Calculate distance to the closest point on the rounded corners
    float cornerDistance = max(dist.x - halfSize.x + radius, dist.y - halfSize.y + radius);

    // Use smoothstep to create a smooth transition between the rectangle and the rounded corners
    float smoothStep = 1.0 - smoothstep(0.0, radius, cornerDistance);

    // Use smoothstep to create a smooth transition between the rounded corners and the interior of the rectangle
    float insideSmoothStep = smoothstep(radius, 0.0, cornerDistance);

    // Combine the results to get the final color
    vec4 finalColor = mix(color, vec4(0.0, 0.0, 0.0, 0.0), smoothStep);
    gl_FragColor = mix(finalColor, color, insideSmoothStep);
}
