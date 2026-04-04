#version 150

in vec3 Position;
in vec4 Color;
in vec4 RoundedParams0;
in vec4 RoundedParams1;

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

out vec4 vertexColor;
out vec4 circleParams0;
out vec4 circleParams1;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    circleParams0 = RoundedParams0;
    circleParams1 = RoundedParams1;
}
