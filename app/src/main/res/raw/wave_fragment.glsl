precision mediump float;

varying vec2 v_texCoord;
uniform vec2 iResolution;
uniform float iTime;
uniform float uSpeed;
uniform float uLineCount;
uniform float uAmplitude;
uniform float uYOffset;

const float MAX_LINES = 20.0;

vec3 orangePinkGradient(vec2 uv) {
    vec3 orange = vec3(1.0, 0.5, 0.0);
    vec3 pink = vec3(1.0, 0.0, 0.5);
    vec3 darkOrange = vec3(0.3, 0.1, 0.0);
    vec3 darkPink = vec3(0.2, 0.0, 0.1);

    vec3 bg = mix(darkOrange, darkPink, uv.x + uv.y * 0.5);
    return bg;
}

float wave(vec2 uv, float speed, float thickness, float softness) {
    float falloff = smoothstep(1.0, 0.5, abs(uv.x));
    float y = falloff * sin(iTime * speed + uv.x * 8.0) * uAmplitude - uYOffset;
    return 1.0 - smoothstep(thickness, thickness + softness, abs(uv.y - y));
}

void main() {
    vec2 uv = gl_FragCoord.xy / iResolution.y;
    vec3 col = orangePinkGradient(uv);

    uv -= 0.5;

    vec3 waveColor1 = vec3(1.0, 0.75, 0.3);
    vec3 waveColor2 = vec3(1.0, 0.2, 0.6);

    float aa = iResolution.y * 0.000005;

    for (float i = 0.0; i < MAX_LINES; i += 1.0) {
        if (i <= uLineCount) {
            float t = i / (uLineCount - 1.0);
            vec3 lineCol = mix(waveColor1, waveColor2, t);
            float bokeh = pow(t, 3.0);
            float thickness = 0.002;
            float softness = aa + bokeh * 0.15;
            float speed = uSpeed * (0.3 + t * 0.3);

            float w = wave(uv, speed, thickness, softness);
            float amt = max(0.0, pow(1.0 - bokeh, 2.0) * 0.8);
            col += w * lineCol * amt;
        }
    }

    float darkness = 0.3;
    col *= darkness;

    gl_FragColor = vec4(col, 1.0);
}