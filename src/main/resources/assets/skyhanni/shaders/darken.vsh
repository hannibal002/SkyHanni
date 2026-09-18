#version 120

//? if >= 26.3
#extension GL_ARB_separate_shader_objects : require

varying vec4 outColor;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    outColor = gl_Color;
}
