import { useEffect, useRef, useState } from 'react'
import * as d3 from 'd3'

import { type GraphData, type GraphEdge, type GraphNode } from './GraphTypes'

type Props = {
  data: GraphData
  width?: number
  height?: number
  onNodeSelect?: (nodeId: string) => void
}

const MAX_LABEL_LENGTH = 34
const DEFAULT_LINK_DISTANCE = 95
const DEFAULT_NODE_REPULSION = -260

export function GraphView({ data, width = 800, height = 600, onNodeSelect }: Props) {
  const svgRef = useRef<SVGSVGElement | null>(null)
  const [size, setSize] = useState({ width, height })

  useEffect(() => {
    if (!svgRef.current) return

    const svgElement = svgRef.current
    const parent = svgElement.parentElement
    if (!parent) return

    const resizeObserver = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (!entry) return

      setSize({
        width: Math.max(entry.contentRect.width, width),
        height: Math.max(entry.contentRect.height, height),
      })
    })

    resizeObserver.observe(parent)

    return () => resizeObserver.disconnect()
  }, [height, width])

  useEffect(() => {
    if (!svgRef.current) return

    const { width: canvasWidth, height: canvasHeight } = size
    const svg = d3.select(svgRef.current)
    svg.selectAll('*').remove()

    const graphLayer = svg.append('g').attr('class', 'graph-layer')

    const simulation = d3
      .forceSimulation<GraphNode>(data.nodes)
      .force(
        'link',
        d3
          .forceLink<GraphNode, GraphEdge>(data.edges)
          .id((d) => d.id)
          .distance(DEFAULT_LINK_DISTANCE)
          .strength((d) => Math.min(Math.max(d.score ?? 0.2, 0.2), 0.95)),
      )
      .force('charge', d3.forceManyBody().strength(DEFAULT_NODE_REPULSION))
      .force('center', d3.forceCenter(canvasWidth / 2, canvasHeight / 2))
      .force('x', d3.forceX(canvasWidth / 2).strength(0.07))
      .force('y', d3.forceY(canvasHeight / 2).strength(0.07))
      .force('collision', d3.forceCollide<GraphNode>().radius(32).strength(1))

    const zoom = d3
      .zoom<SVGSVGElement, unknown>()
      .scaleExtent([0.35, 3])
      .translateExtent([
        [-canvasWidth * 2, -canvasHeight * 2],
        [canvasWidth * 3, canvasHeight * 3],
      ])
      .on('zoom', (event) => {
        graphLayer.attr('transform', event.transform)
      })

    svg.call(zoom)

    const link = graphLayer
      .append('g')
      .attr('stroke', '#6f7483')
      .attr('stroke-opacity', 0.68)
      .selectAll('line')
      .data(data.edges)
      .join('line')
      .attr('stroke-width', (d) => Math.max((d.score ?? 0.25) * 4, 1.5))

    const node = graphLayer
      .append('g')
      .selectAll('circle')
      .data(data.nodes)
      .join('circle')
      .attr('r', 14)
      .attr('fill', '#2563eb')
      .attr('stroke', '#ffffff')
      .attr('stroke-width', 1.5)
      .style('cursor', 'pointer')
      .on('click', (_, nodeData) => {
        onNodeSelect?.(nodeData.id)
      })
      .call(
        d3
          .drag<SVGCircleElement, GraphNode>()
          .on('start', dragStarted)
          .on('drag', dragged)
          .on('end', dragEnded),
      )

    node
      .append('title')
      .text((d) => `${d.label}${d.genre ? `\nGenre: ${d.genre}` : ''}`)

    const label = graphLayer
      .append('g')
      .selectAll('text')
      .data(data.nodes)
      .join('text')
      .text((d) => truncateLabel(d.label))
      .attr('font-size', 12)
      .attr('font-weight', 600)
      .attr('fill', '#0f172a')
      .attr('stroke', 'rgba(255,255,255,0.92)')
      .attr('stroke-width', 4)
      .attr('paint-order', 'stroke')
      .attr('text-anchor', 'start')
      .attr('dx', 18)
      .attr('dy', 4)
      .style('cursor', 'pointer')
      .on('click', (_, nodeData) => {
        onNodeSelect?.(nodeData.id)
      })

    label
      .append('title')
      .text((d) => `${d.label}${d.genre ? `\nGenre: ${d.genre}` : ''}`)

    label.call(
      d3
        .drag<SVGTextElement, GraphNode>()
        .on('start', dragStarted)
        .on('drag', dragged)
        .on('end', dragEnded),
    )

    simulation.on('tick', () => {
      link
        .attr('x1', (d) => (d.source as GraphNode).x!)
        .attr('y1', (d) => (d.source as GraphNode).y!)
        .attr('x2', (d) => (d.target as GraphNode).x!)
        .attr('y2', (d) => (d.target as GraphNode).y!)

      node
        .attr('cx', (d) => d.x!)
        .attr('cy', (d) => d.y!)

      label
        .attr('x', (d) => d.x!)
        .attr('y', (d) => d.y!)
    })

    function dragStarted(event: d3.D3DragEvent<SVGCircleElement, GraphNode, GraphNode>, d: GraphNode) {
      if (!event.active) simulation.alphaTarget(0.3).restart()
      d.fx = d.x
      d.fy = d.y
    }

    function dragged(event: d3.D3DragEvent<SVGCircleElement, GraphNode, GraphNode>, d: GraphNode) {
      d.fx = event.x
      d.fy = event.y
    }

    function dragEnded(event: d3.D3DragEvent<SVGCircleElement, GraphNode, GraphNode>, d: GraphNode) {
      if (!event.active) simulation.alphaTarget(0)
      d.fx = null
      d.fy = null
    }

    return () => {
      simulation.stop()
    }
  }, [data, onNodeSelect, size])

  return <svg ref={svgRef} width={size.width} height={size.height} className="h-full w-full" />
}

function truncateLabel(text: string) {
  if (!text) return 'Untitled note'
  if (text.length <= MAX_LABEL_LENGTH) return text
  return `${text.slice(0, MAX_LABEL_LENGTH - 3)}...`
}
