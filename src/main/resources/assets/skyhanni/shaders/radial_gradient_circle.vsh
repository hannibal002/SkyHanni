#version 150

//~ if >= 26.3 'in vec3 Position;' -> 'layout(location = 0) in vec3 Position;'
in vec3 Position;

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
