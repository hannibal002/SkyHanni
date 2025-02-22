#version 120

const float pi = 3.1415926535897932384626433832795028841971693993751058209749445923078164062f;

uniform float scaleFactor;
uniform float radius;
uniform float smoothness;
uniform vec2 centerPos;
uniform float angle1;
uniform float angle2;

varying vec4 color;

void main() {
    float xScale = gl_ModelViewMatrix[0][0];
    float yScale = gl_ModelViewMatrix[1][1];
    float xTranslation = gl_ModelViewMatrix[3][0];
    float yTranslation = gl_ModelViewMatrix[3][1];

    vec2 cords = vec2(gl_FragCoord.x, gl_FragCoord.y);

    vec2 newCenterPos = vec2((centerPos.x + (radius * (xScale - 1.0))) + (xTranslation * scaleFactor), (centerPos.y - (radius * (yScale - 1.0))) - (yTranslation * scaleFactor));

    float newRadius = radius * min(xScale,yScale);

    vec2 adjusted = cords - newCenterPos;

    float smoothed = 1.0 - smoothstep(pow(newRadius - smoothness,2.0), pow(newRadius,2.0), pow(adjusted.x, 2.0) + pow(adjusted.y, 2.0));

    float current = atan(adjusted.y,adjusted.x);

    float sanity = step(angle1,angle2);

    float lim1 = step(current,angle1);
    float lim2 = step(angle2,current);

    float lim3 = step(angle1,current);
    float lim4 = step(current,angle2);

    float lim = max(lim1,lim2)*sanity+(1.0-sanity)*(1.0-max(lim3,lim4));

    gl_FragColor = color * vec4(1.0, 1.0, 1.0, smoothed*lim);
}
