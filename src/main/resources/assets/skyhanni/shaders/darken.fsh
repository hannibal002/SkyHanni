#version 120

// Fragment Shader
uniform float darknessLevel; // Darkness level

void main()
{
    // Make the object darker
    vec4 color = gl_Color;
    vec3 darkenedColor = color.rgb * darknessLevel;
    gl_FragColor = vec4(darkenedColor, color.a);
}
