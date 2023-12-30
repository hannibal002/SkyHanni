precision mediump float;

varying vec2 v_texCoord;

uniform float width;
uniform float height;
uniform float roundness;
uniform vec4 color;

void main()
{
    // Calculate the distance from the center of the rectangle
    vec2 center = vec2(width, height) * 0.5;
    vec2 distance = abs(v_texCoord - center);

    // Calculate the distance from the center to the corner of the rounded rectangle
    float cornerRadius = min(roundness, min(width, height) * 0.5);
    vec2 roundedDistance = max(distance - vec2(width, height) * 0.5 + cornerRadius, vec2(0.0));

    // Calculate the alpha value based on the distance from the center and the corner radius
    float alpha = smoothstep(cornerRadius, cornerRadius + 0.01, length(roundedDistance));

    // Output the final color with alpha blending
    gl_FragColor = vec4(color.rgb, color.a * alpha);
}
