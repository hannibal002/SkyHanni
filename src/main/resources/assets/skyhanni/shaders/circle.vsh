#version 150

in vec3 Position;
in vec4 Color;
//? < 1.21.6 {
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
//?} else {
/*
// Since there is still a GlslPreprocessor in 1.21.5 we cant have the #moj_import's commented out
// here, since the regex Minecraft uses to find #moj_import lines still detect it when commented out,
// and hence tries to process it. So we just copy them in ourselves.
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
