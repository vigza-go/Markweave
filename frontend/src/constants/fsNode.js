export const FS_NODE_TYPE = {
  FILE: 1,
  FOLDER: 2,
  SHORTCUT: 3
};

export const FILE_TYPE_OPTIONS = [
  { label: 'Markdown 文档', value: FS_NODE_TYPE.FILE },
  { label: '文件夹', value: FS_NODE_TYPE.FOLDER }
];

export const isFolder = (node) => Number(node?.type) === FS_NODE_TYPE.FOLDER;
export const isFile = (node) => Number(node?.type) === FS_NODE_TYPE.FILE;
export const isShortcut = (node) => Number(node?.type) === FS_NODE_TYPE.SHORTCUT;
