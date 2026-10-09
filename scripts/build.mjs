import { cp, mkdir } from 'node:fs/promises';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = fileURLToPath(new URL('../', import.meta.url));
const sourceRoot = join(projectRoot, 'src');
const outputRoot = join(projectRoot, 'dist');

await mkdir(join(outputRoot, 'app'), { recursive: true });
await mkdir(join(outputRoot, 'assets'), { recursive: true });
await cp(join(projectRoot, 'public', 'index.html'), join(outputRoot, 'index.html'));
await cp(join(sourceRoot, 'main.js'), join(outputRoot, 'app', 'main.js'));
for (const folder of ['audio', 'timer', 'data', 'storage']) {
  await cp(join(sourceRoot, folder), join(outputRoot, 'app', folder), { recursive: true });
}
await cp(join(sourceRoot, 'styles', 'main.css'), join(outputRoot, 'assets', 'main.css'));

console.log('Kitchen Mood Radio build complete: dist/');
