#version 150

in vec3 Position;
//? < 1.21.6 {
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
//?} else {
//#moj_import <minecraft:dynamictransforms.glsl>
//#moj_import <minecraft:projection.glsl>
//?}
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
