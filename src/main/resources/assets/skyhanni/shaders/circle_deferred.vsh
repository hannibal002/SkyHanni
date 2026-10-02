#version 150

//~ if >= 26.3 'in vec3 Position;' -> 'layout(location = 0) in vec3 Position;'
in vec3 Position;
//~ if >= 26.3 'in vec4 Color;' -> 'layout(location = 1) in vec4 Color;'
in vec4 Color;
//~ if >= 26.3 'in vec4 RoundedParams0;' -> 'layout(location = 2) in vec4 RoundedParams0;'
in vec4 RoundedParams0;
//~ if >= 26.3 'in vec4 RoundedParams1;' -> 'layout(location = 3) in vec4 RoundedParams1;'
in vec4 RoundedParams1;

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

//~ if >= 26.3 'out vec4 vertexColor;' -> 'layout(location = 0) out vec4 vertexColor;'
out vec4 vertexColor;
//~ if >= 26.3 'out vec4 roundedParams0;' -> 'layout(location = 1) out vec4 roundedParams0;'
out vec4 roundedParams0;
//~ if >= 26.3 'out vec4 roundedParams1;' -> 'layout(location = 2) out vec4 roundedParams1;'
out vec4 roundedParams1;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    roundedParams0 = RoundedParams0;
    roundedParams1 = RoundedParams1;
}
