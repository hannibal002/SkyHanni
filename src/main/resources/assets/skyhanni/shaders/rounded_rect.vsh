#version 150

in vec3 Position;
in vec4 Color;
//? < 1.21.6 {
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
//?} else {
/*
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
    float LineWidth;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};
*///?}
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
}
