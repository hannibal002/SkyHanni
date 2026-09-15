#version 120

//? if >= 26.3
#extension GL_ARB_separate_shader_objects : require

// Fragment Shader
uniform sampler2D texture;
uniform float darknessLevel;

varying vec4 outColor;

void main() {
    vec4 color = texture2D(texture, gl_TexCoord[0].st) * outColor;
    vec3 darkenedColor = color.rgb * darknessLevel;
    gl_FragColor = vec4(darkenedColor, color.a);
}
