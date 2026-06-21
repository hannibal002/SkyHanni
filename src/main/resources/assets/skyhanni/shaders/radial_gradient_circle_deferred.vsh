#version 150

in vec3 Position;
in vec4 RoundedParams0;
in vec4 RoundedParams1;
in vec4 GradientParams0;
in vec4 GradientParams1;
in vec4 GradientParams2;

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

out vec4 roundedParams0;
out vec4 roundedParams1;
out vec4 gradientParams0;
out vec4 gradientParams1;
out vec4 gradientParams2;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    roundedParams0 = RoundedParams0;
    roundedParams1 = RoundedParams1;
    gradientParams0 = GradientParams0;
    gradientParams1 = GradientParams1;
    gradientParams2 = GradientParams2;
}
