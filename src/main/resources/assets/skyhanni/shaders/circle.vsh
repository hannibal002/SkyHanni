#version 150

//~ if >= 26.3 'in vec3 Position;' -> 'layout(location = 0) in vec3 Position;'
in vec3 Position;
//~ if >= 26.3 'in vec4 Color;' -> 'layout(location = 1) in vec4 Color;'
in vec4 Color;

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

//~ if >= 26.3 'out vec4 vertexColor;' -> 'layout(location = 0) out vec4 vertexColor;'
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
}
