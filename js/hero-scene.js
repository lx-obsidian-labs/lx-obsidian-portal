void (function () {
  'use strict';

  var container = document.getElementById('heroCanvas');
  if (!container) return;

  var isMobile = window.innerWidth < 700;
  var PARTICLE_COUNT = isMobile ? 120 : 350;
  var LINE_DISTANCE = isMobile ? 120 : 180;
  var MOUSE_INFLUENCE = 0.15;

  var scene = new THREE.Scene();
  var camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 1000);
  camera.position.z = 300;

  var renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setClearColor(0x000000, 0);
  container.appendChild(renderer.domElement);

  // Particles
  var geometry = new THREE.BufferGeometry();
  var positions = new Float32Array(PARTICLE_COUNT * 3);
  var velocities = new Float32Array(PARTICLE_COUNT * 3);
  var opacities = new Float32Array(PARTICLE_COUNT);
  var sizes = new Float32Array(PARTICLE_COUNT);

  for (var i = 0; i < PARTICLE_COUNT; i++) {
    var i3 = i * 3;
    positions[i3] = (Math.random() - 0.5) * 700;
    positions[i3 + 1] = (Math.random() - 0.5) * 500;
    positions[i3 + 2] = (Math.random() - 0.5) * 400;
    velocities[i3] = (Math.random() - 0.5) * 0.3;
    velocities[i3 + 1] = (Math.random() - 0.5) * 0.3;
    velocities[i3 + 2] = (Math.random() - 0.5) * 0.2;
    opacities[i] = 0.3 + Math.random() * 0.7;
    sizes[i] = 1.5 + Math.random() * 2.5;
  }

  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
  geometry.setAttribute('opacity', new THREE.BufferAttribute(opacities, 1));
  geometry.setAttribute('size', new THREE.BufferAttribute(sizes, 1));

  var vertexShader = [
    'attribute float opacity;',
    'attribute float size;',
    'varying float vOpacity;',
    'void main() {',
    '  vOpacity = opacity;',
    '  vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);',
    '  gl_PointSize = size * (200.0 / -mvPosition.z);',
    '  gl_Position = projectionMatrix * mvPosition;',
    '}'
  ].join('\n');

  var fragmentShader = [
    'varying float vOpacity;',
    'void main() {',
    '  float d = length(gl_PointCoord - vec2(0.5));',
    '  if (d > 0.5) discard;',
    '  float glow = 1.0 - smoothstep(0.0, 0.5, d);',
    '  vec3 color = mix(vec3(0.486, 0.361, 1.0), vec3(0.416, 0.878, 1.0), glow * 0.4);',
    '  gl_FragColor = vec4(color, glow * vOpacity * 0.7);',
    '}'
  ].join('\n');

  var material = new THREE.ShaderMaterial({
    vertexShader: vertexShader,
    fragmentShader: fragmentShader,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending
  });

  var particles = new THREE.Points(geometry, material);
  scene.add(particles);

  // Lines
  var lineGeometry = new THREE.BufferGeometry();
  var linePositions = new Float32Array(PARTICLE_COUNT * PARTICLE_COUNT * 6);
  var lineColors = new Float32Array(PARTICLE_COUNT * PARTICLE_COUNT * 6);
  lineGeometry.setAttribute('position', new THREE.BufferAttribute(linePositions, 3));
  lineGeometry.setAttribute('color', new THREE.BufferAttribute(lineColors, 3));

  var lineMaterial = new THREE.LineBasicMaterial({
    vertexColors: true,
    transparent: true,
    opacity: 0.12,
    blending: THREE.AdditiveBlending,
    depthWrite: false
  });

  var lines = new THREE.LineSegments(lineGeometry, lineMaterial);
  scene.add(lines);

  // Geometric accents
  var shapes = [];
  var shapeGeos = [
    new THREE.IcosahedronGeometry(40, 1),
    new THREE.OctahedronGeometry(30, 0),
    new THREE.TetrahedronGeometry(25, 0)
  ];

  var shapeMat = new THREE.MeshBasicMaterial({
    color: 0x7c5cff,
    wireframe: true,
    transparent: true,
    opacity: 0.08
  });

  for (var s = 0; s < 3; s++) {
    var mesh = new THREE.Mesh(shapeGeos[s], shapeMat.clone());
    mesh.position.set(
      (Math.random() - 0.5) * 400,
      (Math.random() - 0.5) * 200,
      (Math.random() - 0.5) * 200 - 100
    );
    mesh.userData.rotSpeed = { x: (Math.random() - 0.5) * 0.003, y: (Math.random() - 0.5) * 0.005 };
    mesh.material.opacity = 0.04 + Math.random() * 0.06;
    scene.add(mesh);
    shapes.push(mesh);
  }

  // Mouse
  var mouse = { x: 0, y: 0, tx: 0, ty: 0 };

  document.addEventListener('mousemove', function (e) {
    mouse.tx = (e.clientX / window.innerWidth - 0.5) * 2;
    mouse.ty = (e.clientY / window.innerHeight - 0.5) * 2;
  });

  // Resize
  window.addEventListener('resize', function () {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  });

  // Animate
  var lineIndex = 0;
  var frameCount = 0;

  function animate() {
    requestAnimationFrame(animate);
    frameCount++;

    mouse.x += (mouse.tx - mouse.x) * 0.03;
    mouse.y += (mouse.ty - mouse.y) * 0.03;

    camera.position.x = mouse.x * 30;
    camera.position.y = -mouse.y * 20;
    camera.lookAt(0, 0, 0);

    var pos = geometry.attributes.position.array;

    for (var i = 0; i < PARTICLE_COUNT; i++) {
      var i3 = i * 3;
      pos[i3] += velocities[i3];
      pos[i3 + 1] += velocities[i3 + 1];
      pos[i3 + 2] += velocities[i3 + 2];

      if (Math.abs(pos[i3]) > 350) velocities[i3] *= -1;
      if (Math.abs(pos[i3 + 1]) > 250) velocities[i3 + 1] *= -1;
      if (Math.abs(pos[i3 + 2]) > 200) velocities[i3 + 2] *= -1;
    }

    geometry.attributes.position.needsUpdate = true;

    // Update lines every 3rd frame for performance
    if (frameCount % 3 === 0) {
      lineIndex = 0;
      for (var a = 0; a < PARTICLE_COUNT; a++) {
        var ax = pos[a * 3];
        var ay = pos[a * 3 + 1];
        var az = pos[a * 3 + 2];
        for (var b = a + 1; b < PARTICLE_COUNT; b++) {
          var bx = pos[b * 3];
          var by = pos[b * 3 + 1];
          var bz = pos[b * 3 + 2];
          var dx = ax - bx;
          var dy = ay - by;
          var dz = az - bz;
          var dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
          if (dist < LINE_DISTANCE) {
            var fade = 1 - dist / LINE_DISTANCE;
            linePositions[lineIndex] = ax;
            linePositions[lineIndex + 1] = ay;
            linePositions[lineIndex + 2] = az;
            linePositions[lineIndex + 3] = bx;
            linePositions[lineIndex + 4] = by;
            linePositions[lineIndex + 5] = bz;
            lineColors[lineIndex] = 0.486 * fade;
            lineColors[lineIndex + 1] = 0.361 * fade;
            lineColors[lineIndex + 2] = 1.0 * fade;
            lineColors[lineIndex + 3] = 0.416 * fade;
            lineColors[lineIndex + 4] = 0.878 * fade;
            lineColors[lineIndex + 5] = 1.0 * fade;
            lineIndex += 6;
          }
        }
      }
      lineGeometry.setDrawRange(0, lineIndex / 3);
      lineGeometry.attributes.position.needsUpdate = true;
      lineGeometry.attributes.color.needsUpdate = true;
    }

    // Rotate shapes
    for (var s = 0; s < shapes.length; s++) {
      shapes[s].rotation.x += shapes[s].userData.rotSpeed.x;
      shapes[s].rotation.y += shapes[s].userData.rotSpeed.y;
    }

    renderer.render(scene, camera);
  }

  animate();
})();
