#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
//? < 1.21.6 {
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
//?} else {
//#moj_import <minecraft:dynamictransforms.glsl>
//#moj_import <minecraft:projection.glsl>
//?}
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // Pass the color & texture coords to the fragment shader
    vertexColor = Color;
    texCoord0 = UV0;
}
